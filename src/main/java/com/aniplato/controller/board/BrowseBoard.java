package com.aniplato.controller.board;

import com.aniplato.controller.FormSupport;
import com.aniplato.controller.TableRows;
import com.aniplato.model.FoodDetail;
import com.aniplato.model.Offer;
import com.aniplato.model.Post;
import com.aniplato.service.BoardService;
import com.aniplato.service.BoardService.BrowseFigures;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * The browse board: a donor's Browse Requests and a food group's Browse Listings are the same screen with different
 * columns and wording. Open posts of the viewer's city come first, in a band of their own (R2.3).
 */
public final class BrowseBoard {

    /** Which posts are on the board; it fixes the wording and the columns. */
    public enum Kind {
        REQUESTS("Browse Requests", "request", "REQUEST", "GROUP", "NEEDED-BY", "OPEN REQUESTS", "KG NEEDED",
                "Search request or group…", "Your city first, then other cities, soonest needed-by first within each. A request leaves the board once the food group accepts an offer."),
        LISTINGS("Browse Listings", "listing", "FOOD", "DONOR", "USE-BY", "OPEN SURPLUS", "KG AVAILABLE",
                "Search food or donor…", "Your city first, then other cities, soonest use-by first within each. A listing leaves the board once the donor accepts an offer.");

        final String title;
        final String noun;
        final String firstColumn;
        final String posterColumn;
        final String dueColumn;
        final String countTitle;
        final String kgTitle;
        final String searchPrompt;
        final String footnote;

        Kind(String title, String noun, String firstColumn, String posterColumn, String dueColumn, String countTitle,
             String kgTitle, String searchPrompt, String footnote) {
            this.title = title;
            this.noun = noun;
            this.firstColumn = firstColumn;
            this.posterColumn = posterColumn;
            this.dueColumn = dueColumn;
            this.countTitle = countTitle;
            this.kgTitle = kgTitle;
            this.searchPrompt = searchPrompt;
            this.footnote = footnote;
        }
    }

    private final Kind kind;
    private final Board<Post> board;
    private final BoardService figures = new BoardService();
    private final Consumer<Post> onOffer;
    private Set<Integer> offered = Set.of();
    private String myCity = "";

    /** @param refresh reloads the board (the Refresh button); @param onOffer opens the Make Offer form for a post */
    public BrowseBoard(Kind kind, Runnable refresh, Consumer<Post> onOffer) {
        this.kind = kind;
        this.onOffer = onOffer;
        boolean requests = kind == Kind.REQUESTS;
        Button refreshButton = new Button("Refresh");
        refreshButton.getStyleClass().add("primary-button");
        refreshButton.setOnAction(e -> refresh.run());
        double[] widths = requests ? new double[] {21, 17, 15, 14, 7, 14, 12} : new double[] {27, 20, 16, 8, 17, 12};
        String[] titles = requests
                ? new String[] {kind.firstColumn, kind.posterColumn, "ACCEPTS", "CITY", "KG", kind.dueColumn, ""}
                : new String[] {kind.firstColumn, kind.posterColumn, "CITY", "KG", kind.dueColumn, ""};
        board = new Board<Post>(kind.title)
                .action(refreshButton)
                .tabs(List.of(new Board.Tab<>("All", p -> true),
                        new Board.Tab<>("Today", p -> due(p).equals(LocalDate.now())),
                        new Board.Tab<>("Tomorrow", p -> due(p).equals(LocalDate.now().plusDays(1)))))
                .cities(p -> p.city)
                .search(kind.searchPrompt, p -> p.title() + " " + p.posterName)
                .columns(widths, titles)
                .bands(p -> p.city.equals(myCity) ? "mine" : "other",
                        (key, n) -> (key.equals("mine") ? "IN " + myCity.toUpperCase(Locale.ROOT) : "OTHER CITIES")
                                + " · " + n + " " + kind.noun.toUpperCase(Locale.ROOT) + (n == 1 ? "" : "S"))
                .row(this::row)
                .footnote(kind.footnote)
                .empty(requests ? "No requests are open right now." : "No listings are open right now.");
    }

    /** The posts the viewer already has an offer on; a Withdrawn offer may be sent again, so it does not count. */
    public static Set<Integer> offeredIds(List<Offer> mine) {
        Set<Integer> ids = new HashSet<>();
        for (Offer offer : mine) {
            if (offer.status != Offer.Status.WITHDRAWN) ids.add(offer.postId);
        }
        return ids;
    }

    public Node view() {
        return board.view();
    }

    /** The rows now shown after the tab, city and search. Tests read this. */
    public List<Post> visible() {
        return board.visible();
    }

    /** Draws the board. {@code alreadyOffered} are the posts the viewer has an offer on (shown as a disabled "Offered"). */
    public void show(List<Post> posts, Set<Integer> alreadyOffered) {
        offered = alreadyOffered;
        board.tiles(tiles(posts));
        board.show(posts);
    }

    private List<Tile> tiles(List<Post> posts) {
        BrowseFigures f = FormSupport.load(() -> figures.browse(posts), null);
        if (f == null) return List.of();
        myCity = f.myCity();
        boolean today = f.soonestAt() != null && f.soonestAt().toLocalDate().equals(LocalDate.now());
        String closest = f.soonestAt() == null ? "—"
                : today ? FormSupport.soon(f.soonestAt(), LocalDate.now()).substring("Today, ".length())
                : FormSupport.soon(f.soonestAt(), LocalDate.now());
        String closestTitle = (kind == Kind.REQUESTS ? "CLOSEST NEEDED-BY" : "CLOSEST USE-BY") + (today ? " · TODAY" : "");
        String closestNote = f.soonest() == null ? "Nothing is waiting" : f.soonest().title();
        String where = f.inMyCity() + " in " + f.myCity() + " · " + f.elsewhere() + " elsewhere";
        return List.of(
                Tile.of(Tile.Icon.BLUE, kind.countTitle, String.valueOf(f.open()), where),
                Tile.of(Tile.Icon.ORANGE, closestTitle, closest, closestNote),
                Tile.kg(Tile.Icon.GREEN, kind.kgTitle, f.kg(), "Across " + f.open() + " open " + kind.noun + (f.open() == 1 ? "" : "s")));
    }

    private GridPane row(Post post) {
        boolean requests = kind == Kind.REQUESTS;
        double[] widths = requests ? new double[] {21, 17, 15, 14, 7, 14, 12} : new double[] {27, 20, 16, 8, 17, 12};
        GridPane row = TableRows.row(widths);
        int column = 0;
        TableRows.put(row, column++, TableRows.text(post.title(), "form-label"));
        TableRows.put(row, column++, TableRows.text(post.posterName, "table-cell-text"));
        if (requests) TableRows.put(row, column++, TableRows.text(accepts(post), "table-cell-text"));
        TableRows.put(row, column++, TableRows.text(post.city, "table-cell-text"));
        TableRows.put(row, column++, TableRows.text(FormSupport.kg(post.kg), "table-cell-text"));
        TableRows.put(row, column++, TableRows.text(FormSupport.soon(BoardService.dueAt(post), LocalDate.now()), "table-cell-muted"));
        Button action = TableRows.rowButton(offered.contains(post.id) ? "Offered" : "Make offer", () -> onOffer.accept(post));
        action.setDisable(offered.contains(post.id));
        TableRows.put(row, column, action);
        return row;
    }


    private static String accepts(Post post) {
        return post.use.acceptedCategories.stream().map(FoodDetail.Category::label).sorted().collect(Collectors.joining(", "));
    }

    private static LocalDate due(Post post) {
        return BoardService.dueAt(post).toLocalDate();
    }
}
