# Running your own ANIPLATO

Written for: someone who downloaded the program from the project's Releases page and wants their own copy, with their own
database. Nothing here uses the project team's database or keys.

ANIPLATO stores everything in one MySQL database. You bring that database; the program carries its own Java. Each person's data
stays in their own database, so two people who each set up their own copy do not see each other.

## What you need

| Need | Required | Notes |
|---|---|---|
| Windows 10 or 11 | yes | |
| A MySQL database, 8.0.16 or newer | yes | anywhere the computer can reach; see below |
| A free key for the help assistant | no | https://aistudio.google.com/app/apikey. Without it the assistant says it is not set up. |
| A Gmail app password | no | https://myaccount.google.com/apppasswords. Without it no emails go out. |

MariaDB is not supported: the tables use CHECK constraints from MySQL 8.0.16.

### Where the MySQL can be

Any MySQL works, as long as you can give the app a host, a port, a user and a password. Two common routes:

- **On your own computer.** Install MySQL Community Server (free) from mysql.com and note the root password you choose. Host is
  `localhost`, port `3306`.
- **Hosted.** Any provider that offers MySQL 8 gives you the same four details, and often a certificate file (`ca.pem`). Use
  the certificate field on the Setup screen when you have one.

The app does not care which. It sees only the four details.

### A database user of its own (recommended)

Using `root` works for a personal copy. If you would rather give the app a limited user, run this in MySQL as an administrator
(change the password):

```sql
CREATE DATABASE aniplato CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'aniplato'@'localhost' IDENTIFIED BY 'choose-a-password';
GRANT ALL PRIVILEGES ON aniplato.* TO 'aniplato'@'localhost';
```

Then enter `aniplato` and that password on the Setup screen. (For a hosted database use `'%'` or the host your provider says.)

## First run

1. Unzip the folder anywhere and keep its contents together. Double-click `ANIPLATO.exe`. If Windows shows "Windows protected
   your PC", choose **More info**, then **Run anyway**: the program is not code-signed.
2. The **Setup** screen opens when the program has no database login saved. Enter host, port, database name, user and
   password (or paste a `jdbc:mysql://host:3306/name` address and click **Use address**). Add the certificate file only if your
   provider gave you one.
3. Click **Test connection**. It tells you one of:
   - **Empty:** ready. Enter the email and password for your admin account (at least 8 characters) and click **Create**. This
     creates the database if it is missing and the user is allowed to, then the tables and the Metro Manila city list, then your
     admin.
   - **Already set up:** ANIPLATO is already in that database, for example from another computer. Click **Connect**. It saves
     the settings and changes nothing in the database.
   - **Cannot continue:** one plain sentence says why. Nothing was changed.
4. Sign in as the admin. Register a donor and a food group from the sign-in screen, then approve them under the admin's
   pending accounts. The board starts empty.

**Setup never deletes or replaces data.** If it finds only some of ANIPLATO's tables it stops and says so. If an earlier setup
failed halfway, it removes the tables it had just made, so you can run it again.

### Optional: assistant and email

Open the Setup screen's "Optional" section, or add the lines later to your settings file (below). The assistant sends only your
question and the screen name, never account data.

## Your settings file

`%APPDATA%\ANIPLATO\db.properties` (paste that into the Explorer address bar). It holds the database password in plain text, so
keep it private. The program reads it at start, so close and reopen after editing. A `db.properties` beside `ANIPLATO.exe` also
works; the one in `%APPDATA%` is used first.

To use a different database, choose **Change database settings** on the sign-in screen after a database problem, or delete the
file and start the program again.

## Setting up by hand

For people who prefer SQL, or whose host does not allow the Setup screen's steps:

1. In your MySQL, select a database and run `sql\schema.sql` (tables and cities only; it drops nothing).
2. Run `sql\seed.sql`. It creates one admin, `admin@aniplato.test` with the password `Aniplato123!`, and the program makes you
   choose a new password at the first sign-in. Do not leave a database public with this account unchanged.
3. Copy `db.properties.example` to `db.properties` and fill in `url`, `user` and `password`.

## Moving or backing up

Everything is in the database, including uploaded photos and documents. Back it up with `mysqldump aniplato > backup.sql` and
restore it into a new database. On a second computer, run the program, enter the same details, and choose **Connect**.

## If something goes wrong

| What you see | What it means |
|---|---|
| Cannot reach host:port | wrong address or port, no internet, or MySQL is not running |
| The database did not accept that user name and password | wrong login |
| not allowed to create the database or tables | use a user with permission, or create an empty database yourself and grant the user rights on it |
| already has some of the tables ANIPLATO uses | the database holds part of an old copy; use an empty one or another name |
| ANIPLATO needs MySQL 8.0.16 or newer | upgrade MySQL |
| The secure connection failed | the certificate file is not the one your provider gave you |
| Registration emails do not arrive | no email settings, or a wrong app password; the `email_log` table records each failure |

## For developers

A source checkout uses `setup-db.ps1` (with `-Demo` for sample data) and `src/main/resources/db.properties`; see the README.
`sql/reset.sql` drops and recreates `aniplato` and is for developers only.
