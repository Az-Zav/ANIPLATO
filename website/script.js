/* ANIPLATO standalone webpage. No npm, React, or build step required.
 * An optional backend URL may be set below. Empty = browser-only demo feedback.
 */
'use strict';
const FEEDBACK_ENDPOINT = '';
const CONTENT = {
  "roles": [
    {
      "name": "Business donors",
      "label": "Turn surplus into possibility.",
      "text": "Restaurants, bakeries, canteens, caterers, and groceries can post surplus raw, packaged, baked, and cooked food within the platform’s food rules.",
      "detail": "Verification: Mayor’s Permit + Sanitary Permit",
      "icon": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"37\" height=\"37\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\" class=\"lucide lucide-package\" aria-hidden=\"true\"><path d=\"M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z\"></path><path d=\"M12 22V12\"></path><polyline points=\"3.29 7 12 12 20.71 7\"></polyline><path d=\"m7.5 4.27 9 5.15\"></path></svg>"
    },
    {
      "name": "Casual donors",
      "label": "Good food can start at home.",
      "text": "Households, event hosts, market stalls, and small farms can share raw and sealed-packaged food. Each listing includes its quantity, pickup window, and use-by time.",
      "detail": "Verification: Barangay Certificate",
      "icon": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"37\" height=\"37\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\" class=\"lucide lucide-hand-heart\" aria-hidden=\"true\"><path d=\"M11 14h2a2 2 0 0 0 0-4h-3c-.6 0-1.1.2-1.4.6L3 16\"></path><path d=\"m14.45 13.39 5.05-4.694C20.196 8 21 6.85 21 5.75a2.75 2.75 0 0 0-4.797-1.837.276.276 0 0 1-.406 0A2.75 2.75 0 0 0 11 5.75c0 1.2.802 2.248 1.5 2.946L16 11.95\"></path><path d=\"m2 15 6 6\"></path><path d=\"m7 20 1.6-1.4c.3-.4.8-.6 1.4-.6h4c1.1 0 2.1-.4 2.8-1.2l4.6-4.4a1 1 0 0 0-2.75-2.91\"></path></svg>"
    },
    {
      "name": "Food groups",
      "label": "Bring food where it is needed.",
      "text": "Parishes, schools, charities, and volunteer groups with a standing feeding activity can offer to collect surplus or post a food request for an upcoming program.",
      "detail": "Verification: organization credential + authorization letter",
      "icon": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"37\" height=\"37\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\" class=\"lucide lucide-users\" aria-hidden=\"true\"><path d=\"M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2\"></path><path d=\"M16 3.128a4 4 0 0 1 0 7.744\"></path><path d=\"M22 21v-2a4 4 0 0 0-3-3.87\"></path><circle cx=\"9\" cy=\"7\" r=\"4\"></circle></svg>"
    },
    {
      "name": "Administrators",
      "label": "Build trust into every exchange.",
      "text": "Admins review registrations and documents, handle safety notices and flagged reports, manage account status, and track the platform’s outcomes.",
      "detail": "Oversight: document review, safety, and outcome metrics",
      "icon": "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"37\" height=\"37\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"1.5\" stroke-linecap=\"round\" stroke-linejoin=\"round\" class=\"lucide lucide-shield-check\" aria-hidden=\"true\"><path d=\"M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z\"></path><path d=\"m9 12 2 2 4-4\"></path></svg>"
    }
  ],
  "steps": [
    [
      "Get verified",
      "Donors and food groups submit their role-specific documents. An admin reviews the registration before an account can participate."
    ],
    [
      "Post or request",
      "A donor lists surplus food, or a food group describes what its program needs. Photos, quantities, and time windows make the need clear."
    ],
    [
      "Choose an offer",
      "The other side submits an offer. The owner of the listing or request chooses one offer, creating a recorded commitment."
    ],
    [
      "Confirm the handoff",
      "The donor confirms release first. The food group then confirms receipt. ANIPLATO coordinates the exchange; participants arrange transport."
    ],
    [
      "Document the outcome",
      "The food group submits a program report. Posts close with a recorded outcome: rescued, expired, or cancelled."
    ]
  ]
};

// Navigation menu.
const menu = document.querySelector('.menu');
const nav = document.querySelector('nav');
menu.addEventListener('click', () => {
  const open = menu.getAttribute('aria-expanded') !== 'true';
  menu.setAttribute('aria-expanded', String(open));
  menu.setAttribute('aria-label', open ? 'Close menu' : 'Open menu');
  nav.classList.toggle('open', open);
});
nav.querySelectorAll('a').forEach(link => link.addEventListener('click', () => {
  nav.classList.remove('open');
  menu.setAttribute('aria-expanded', 'false');
  menu.setAttribute('aria-label', 'Open menu');
}));
document.addEventListener('keydown', event => {
  if (event.key === 'Escape' && nav.classList.contains('open')) {
    nav.classList.remove('open');
    menu.setAttribute('aria-expanded', 'false');
    menu.setAttribute('aria-label', 'Open menu');
    menu.focus();
  }
});

// Accessible tabs: click, Arrow keys, Home, and End.
function wireTabs(container, onSelect) {
  const tabs = [...container.querySelectorAll('[role="tab"]')];
  function select(index, focus) {
    tabs.forEach((tab, i) => {
      tab.setAttribute('aria-selected', String(i === index));
      tab.tabIndex = i === index ? 0 : -1;
    });
    onSelect(index);
    if (focus) tabs[index].focus();
  }
  tabs.forEach((tab, i) => {
    tab.tabIndex = i === 0 ? 0 : -1;
    tab.addEventListener('click', () => select(i, false));
    tab.addEventListener('keydown', event => {
      let next;
      if (event.key === 'ArrowRight' || event.key === 'ArrowDown') next = (i + 1) % tabs.length;
      if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') next = (i - 1 + tabs.length) % tabs.length;
      if (event.key === 'Home') next = 0;
      if (event.key === 'End') next = tabs.length - 1;
      if (next !== undefined) { event.preventDefault(); select(next, true); }
    });
  });
}

// The two donation routes.
const routes = [
  { title: 'Start with what you can share.', description: 'A donor posts a listing. Food groups send offers to collect it. The donor chooses one group.', cards: [
    ['DONOR', 'Post surplus', 'Add food details, photos, quantity, and a pickup window.'],
    ['FOOD GROUP', 'Offer to collect', 'Explain the intended use and propose a pickup time.'],
    ['TOGETHER', 'Make the handoff', 'Accept one offer, confirm release, then confirm receipt.']
  ]},
  { title: 'Start with what your program needs.', description: 'A food group posts a request. Donors offer suitable food. The group chooses one donor’s offer.', cards: [
    ['FOOD GROUP', 'Post a request', 'Describe the food, quantity, and upcoming feeding program.'],
    ['DONOR', 'Offer suitable food', 'Add food details and a pickup window before the needed-by time.'],
    ['TOGETHER', 'Make the handoff', 'Choose one offer, confirm release, then confirm receipt.']
  ]}
];
wireTabs(document.querySelector('.route-tabs'), index => {
  const panel = document.getElementById('route-panel');
  panel.setAttribute('aria-labelledby', 'route-tab-' + index);
  panel.querySelector('.pill').textContent = 'ROUTE ' + (index + 1);
  panel.querySelector('.route-intro h3').textContent = routes[index].title;
  panel.querySelector('.route-intro p').textContent = routes[index].description;
  panel.querySelectorAll('.route-cards article').forEach((card, i) => {
    const [who, title, description] = routes[index].cards[i];
    card.querySelector('small').textContent = who;
    card.querySelector('h4').textContent = title;
    card.querySelector('p').textContent = description;
  });
});

// Participant roles. Icon SVGs below are trusted, bundled page assets.
wireTabs(document.querySelector('.role-tabs'), index => {
  const panel = document.getElementById('role-panel');
  const role = CONTENT.roles[index];
  panel.setAttribute('aria-labelledby', 'role-tab-' + index);
  panel.querySelector('svg').outerHTML = role.icon;
  panel.querySelector('h3').textContent = role.label;
  panel.querySelector('p').textContent = role.text;
  const verification = panel.querySelector('.verification');
  [...verification.childNodes].filter(node => node.nodeType === Node.TEXT_NODE).forEach(node => node.remove());
  verification.append(document.createTextNode(role.detail));
});

// Workflow stage explorer.
const stageButtons = [...document.querySelectorAll('.step-buttons button')];
stageButtons.forEach((button, index) => button.addEventListener('click', () => {
  stageButtons.forEach((item, i) => {
    item.setAttribute('aria-pressed', String(i === index));
    item.querySelector('span').textContent = i < index ? '✓' : String(i + 1);
  });
  document.querySelector('.step-detail b').textContent = CONTENT.steps[index][0];
  document.querySelector('.step-detail p').textContent = CONTENT.steps[index][1];
}));

// Copy the desktop app launch command.
const copyButton = document.querySelector('.command button');
copyButton.addEventListener('click', async () => {
  try {
    if (!navigator.clipboard) throw new Error('Clipboard unavailable');
    await navigator.clipboard.writeText('mvn javafx:run');
    copyButton.querySelector('span').textContent = 'Copied';
    setTimeout(() => { copyButton.querySelector('span').textContent = 'Copy'; }, 2000);
  } catch {
    copyButton.querySelector('span').textContent = 'Select command to copy';
    const selection = window.getSelection();
    const range = document.createRange();
    range.selectNodeContents(document.querySelector('.command code'));
    selection.removeAllRanges();
    selection.addRange(range);
  }
});

// Feedback. By default this demo stores responses only on the current browser.
let rating = 0;
const form = document.querySelector('.feedback-card form');
const ratingButtons = [...form.querySelectorAll('.stars button')];
const error = form.querySelector('.error');
const submitButton = form.querySelector('.submit');
const submitLabel = submitButton.firstChild;
if (FEEDBACK_ENDPOINT) {
  form.querySelector('small').textContent = 'Feedback will be sent to the configured project endpoint.';
  form.querySelector('.consent').lastChild.textContent = 'I agree to send this feedback for project evaluation.';
  document.querySelector('.feedback-note p').textContent = 'Feedback will be sent to the configured project endpoint. Please leave out personal or sensitive information.';
  submitLabel.textContent = 'Share feedback';
}
ratingButtons.forEach((button, index) => button.addEventListener('click', () => {
  rating = index + 1;
  ratingButtons.forEach((star, i) => {
    star.setAttribute('aria-pressed', String(i === index));
    star.querySelector('svg').setAttribute('fill', i <= index ? 'currentColor' : 'none');
  });
  form.querySelector('.stars > span').textContent = rating + ' / 5';
  error.hidden = true;
}));
form.addEventListener('submit', async event => {
  event.preventDefault();
  error.hidden = true;
  if (!form.reportValidity()) return;
  const values = new FormData(form);
  const comment = String(values.get('comment') || '').trim();
  if (!rating || comment.length < 10 || comment.length > 1500) {
    error.textContent = 'Choose a rating and enter a comment of 10–1,500 characters.';
    error.hidden = false;
    return;
  }
  if (values.get('website')) return;
  const response = {
    audience: values.get('audience'), rating, comment,
    consent: values.get('consent') === 'on', createdAt: new Date().toISOString()
  };
  submitButton.disabled = true;
  submitLabel.textContent = 'Saving…';
  try {
    if (FEEDBACK_ENDPOINT) {
      const result = await fetch(FEEDBACK_ENDPOINT, {
        method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(response)
      });
      if (!result.ok) throw new Error('Feedback could not be sent. Please try again later.');
    } else {
      const existing = JSON.parse(localStorage.getItem('aniplato_demo_feedback') || '[]');
      if (!Array.isArray(existing)) throw new Error('Browser feedback storage is unavailable. Clear this site’s browser data and try again.');
      existing.push(response);
      localStorage.setItem('aniplato_demo_feedback', JSON.stringify(existing));
    }
    form.hidden = true;
    let success = document.querySelector('.feedback-card .success');
    if (!success) {
      success = document.createElement('div');
      success.className = 'success';
      success.setAttribute('role', 'status');
      const title = document.createElement('h3');
      title.textContent = FEEDBACK_ENDPOINT ? 'Thank you for sharing.' : 'Demo feedback saved.';
      const message = document.createElement('p');
      message.textContent = FEEDBACK_ENDPOINT ? 'Your feedback was sent successfully.' : 'Saved in this browser only. This response was not sent to the project team.';
      const again = document.createElement('button');
      again.type = 'button';
      again.className = 'inline-link';
      again.textContent = 'Write another response →';
      again.addEventListener('click', () => {
        form.reset(); rating = 0;
        ratingButtons.forEach(star => {
          star.setAttribute('aria-pressed', 'false');
          star.querySelector('svg').setAttribute('fill', 'none');
        });
        form.querySelector('.stars > span').textContent = 'Choose a rating';
        success.hidden = true; form.hidden = false;
        form.querySelector('select').focus();
      });
      success.append(title, message, again);
      document.querySelector('.feedback-card').append(success);
    }
    success.hidden = false;
  } catch (problem) {
    error.textContent = FEEDBACK_ENDPOINT ? problem.message : 'Feedback could not be saved in this browser. Try opening the page with VS Code Live Server and allow browser storage.';
    error.hidden = false;
  } finally {
    submitButton.disabled = false;
    submitLabel.textContent = FEEDBACK_ENDPOINT ? 'Share feedback' : 'Save demo feedback';
  }
});
