package com.aniplato.service;

import com.aniplato.dao.AccountDao;
import com.aniplato.dao.RegistrationDao;
import com.aniplato.exception.RuleViolationException;
import com.aniplato.model.Account;
import com.aniplato.model.DocumentUpload;
import com.aniplato.model.Registration;
import com.aniplato.util.Database;
import com.aniplato.util.Session;

import java.util.List;

/**
 * What an organization can change about itself (6.3): its contact details and, for a Casual donor, an upgrade to
 * Business. Spec 7.3 lists these under RegistrationService; they are here so that file stays under 300 lines.
 */
public class ProfileService {

    private final RegistrationDao registrationDao = new RegistrationDao();
    private final AccountDao accountDao = new AccountDao();
    private final RegistrationService registrationService = new RegistrationService();
    private final EmailService emailService = new EmailService();

    /** The logged-in organization's own contact number, to prefill the edit form. Only the owner asks (A7). */
    public String getMyPhone() {
        Account me = Session.require();
        return Database.inTransaction("Could not read your details", c -> accountDao.findContactPhone(c, me.id));
    }

    /** Changes the contact person and phone of the logged-in organization. The phone follows the registration rule (A7). */
    public void updateContact(String contactPerson, String contactPhone) {
        Account me = Session.require();
        if (me.registration == null) throw new RuleViolationException("Only an organization has contact details.");
        String person = contactPerson == null ? "" : contactPerson.trim();
        String phone = contactPhone == null ? "" : contactPhone.trim();
        if (person.isEmpty()) throw new RuleViolationException("Enter the contact person.");
        RegistrationService.requireContactNumber(phone);
        Database.inTransaction("Could not save your contact details", c -> {
            registrationDao.updateContact(c, me.registration.id, person, phone);
            return null;
        });
        me.registration.contactPerson = person;   // the session shows the saved name; it never holds the phone (A7)
    }

    /**
     * A Casual donor uploads both permits to become a Business donor (6.2 Stage 1). It creates a new Pending
     * registration for the same email and details, reviewed like any other; approval changes the role. Until then
     * nothing changes, and a rejected upgrade changes nothing.
     */
    public void submitUpgrade(List<DocumentUpload> uploads, boolean consent) {
        Account me = Session.require();
        if (!me.canUpgradeToBusiness()) throw new RuleViolationException("Only a Casual donor can upgrade to Business.");
        if (!consent) throw new RuleViolationException("Agree to the consent text to continue.");
        registrationService.checkDocuments(Account.Role.BUSINESS_DONOR, uploads);
        boolean waiting = Database.inTransaction("Could not check for an earlier upgrade", c ->
                registrationDao.findByStatus(c, Registration.Status.PENDING).stream().anyMatch(r -> r.email.equalsIgnoreCase(me.email)));
        if (waiting) throw new RuleViolationException("Your upgrade is already waiting for review.");
        Registration upgrade = new Registration();
        upgrade.role = Account.Role.BUSINESS_DONOR;
        upgrade.name = me.registration.name;
        upgrade.city = me.registration.city;
        upgrade.contactPerson = me.registration.contactPerson;
        upgrade.contactPhone = Database.inTransaction("Could not read your details", c -> accountDao.findContactPhone(c, me.id));   // the owner may read its own number (A7)
        upgrade.email = me.email;
        registrationService.save(upgrade, uploads, me.id);
        emailService.sendReceived(upgrade.email, upgrade.name);   // after the commit, like a new registration
    }
}
