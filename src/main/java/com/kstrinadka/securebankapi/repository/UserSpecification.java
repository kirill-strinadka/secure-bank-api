package com.kstrinadka.securebankapi.repository;

import com.kstrinadka.securebankapi.entity.EmailDataEntity;
import com.kstrinadka.securebankapi.entity.PhoneDataEntity;
import com.kstrinadka.securebankapi.entity.UserEntity;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserSpecification {

    private UserSpecification() {
    }

    public static Specification<UserEntity> search(
            LocalDate dateOfBirth,
            String phone,
            String name,
            String email
    ) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (dateOfBirth != null) {
                predicates.add(criteriaBuilder.greaterThan(root.get("dateOfBirth"), dateOfBirth));
            }

            if (StringUtils.hasText(phone)) {
                Join<UserEntity, PhoneDataEntity> phones = root.join("phones");
                predicates.add(criteriaBuilder.equal(phones.get("phone"), phone));
            }

            if (StringUtils.hasText(name)) {
                String prefix = name.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), prefix));
            }

            if (StringUtils.hasText(email)) {
                Join<UserEntity, EmailDataEntity> emails = root.join("emails");
                predicates.add(criteriaBuilder.equal(emails.get("email"), email));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
