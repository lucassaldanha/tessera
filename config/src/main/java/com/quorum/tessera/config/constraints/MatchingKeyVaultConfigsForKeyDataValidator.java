package com.quorum.tessera.config.constraints;

import com.quorum.tessera.config.KeyConfiguration;
import com.quorum.tessera.config.KeyVaultType;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MatchingKeyVaultConfigsForKeyDataValidator
        implements ConstraintValidator<MatchingKeyVaultConfigsForKeyData, KeyConfiguration> {

    @Override
    public boolean isValid(KeyConfiguration keyConfiguration, ConstraintValidatorContext cvc) {

        // return true so that only NotNull annotation creates violation
        if (keyConfiguration == null) {
            return true;
        }

        List<Boolean> outcomes =
                Stream.of(KeyVaultType.values())
                        .filter(k -> {
                            if (Optional.ofNullable(keyConfiguration.getKeyData()).isPresent()) {
                                return keyConfiguration.getKeyData().stream().anyMatch(kd -> k.getKeyPairType().isInstance(kd));
                            }
                            return false;
                        })
                        .filter(k -> !keyConfiguration.getKeyVaultConfig(k).isPresent())
                        .map(
                                k -> {
                                    cvc.disableDefaultConstraintViolation();
                                    String messageKey =
                                            String.format(
                                                    "{MatchingKeyVaultConfigsForKeyData.%s.message}",
                                                    k.name().toLowerCase());
                                    cvc.buildConstraintViolationWithTemplate(messageKey).addConstraintViolation();
                                    return false;
                                })
                        .collect(Collectors.toList());

        return outcomes.isEmpty();
    }
}
