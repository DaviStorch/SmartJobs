package com.storch.smartjobs.service;

import java.util.Map;

public record ResumeImportDraft(Map<String, String> sections, String warning) {
}
