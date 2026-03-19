package ca.yisong.energyops.support;

import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ca.yisong.energyops.api.ApiException;

@Component
public class CanadianLocaleRules {

    private static final Set<String> PROVINCES = Set.of(
            "AB", "BC", "MB", "NB", "NL", "NS", "NT",
            "NU", "ON", "PE", "QC", "SK", "YT"
    );

    public String normalizeProvince(String province) {
        String normalized = province == null ? "" : province.trim().toUpperCase(Locale.CANADA);
        if (!PROVINCES.contains(normalized)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Province or territory code must be a valid Canadian code.");
        }
        return normalized;
    }

    public String normalizePostalCode(String postalCode) {
        String normalized = postalCode == null ? "" : postalCode.trim().toUpperCase(Locale.CANADA).replaceAll("\\s+", "");
        if (!normalized.matches("^[A-Z]\\d[A-Z]\\d[A-Z]\\d$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Postal code must follow the Canadian pattern A1A 1A1.");
        }
        return normalized.substring(0, 3) + " " + normalized.substring(3);
    }
}
