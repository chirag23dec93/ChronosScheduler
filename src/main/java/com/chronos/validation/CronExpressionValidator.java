package com.chronos.validation;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import org.quartz.CronExpression;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CronExpressionValidator.Validator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CronExpressionValidator {
    String message() default "Invalid cron expression";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<CronExpressionValidator, String> {
        @Override
        public void initialize(CronExpressionValidator constraintAnnotation) {
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            if (value == null || value.trim().isEmpty()) {
                return true; // Let @NotNull or @NotEmpty handle this
            }
            try {
                return CronExpression.isValidExpression(value);
            } catch (Exception e) {
                return false;
            }
        }
    }
}
