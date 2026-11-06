package com.aniplato.model;

import java.io.File;
import java.time.LocalDate;

/** One file chosen on the registration form: its expiry date, or the "No expiry" box ticked. */
public record DocumentUpload(Document.Type type, File file, LocalDate expiresOn, boolean noExpiry) { }
