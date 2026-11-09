package com.storch.smartjobs.service;

import com.storch.smartjobs.dto.JobForm;
import org.springframework.stereotype.Component;
import java.util.Arrays;

@Component
public class JobTextParser {
    public JobForm parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Cole o texto da oportunidade para iniciar a importação.");
        }
        if (text.length() > 30000) {
            throw new IllegalArgumentException("O texto ultrapassa o limite de 30.000 caracteres.");
        }
        String[] lines = text.lines().map(String::strip).filter(line -> !line.isBlank()).toArray(String[]::new);
        JobForm form = new JobForm();
        form.setTitle(lines.length == 0 ? "" : lines[0].substring(0, Math.min(lines[0].length(), 180)));
        form.setCompany(Arrays.stream(lines)
                .filter(line -> line.matches("(?i)^(empresa|company)\\s*[:\\-].+"))
                .map(line -> line.replaceFirst("(?i)^(empresa|company)\\s*[:\\-]\\s*", ""))
                .findFirst().orElse(""));
        form.setDescription(text);
        form.setPastedText(text);
        form.setSource("Importação manual");
        return form;
    }
}
