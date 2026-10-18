package com.onlinepandit.marketplace;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface PanditRepository extends JpaRepository<Pandit, Long> {
       List<Pandit> findByApprovedFalseOrderByIdAsc();
       java.util.Optional<Pandit> findByUserId(Long userId);
       java.util.Optional<Pandit> findByStripeAccountId(String stripeAccountId);

    @Query("select p from Pandit p where p.approved = true " +
           "and (:city is null or lower(p.city) = lower(:city)) " +
           "and (:country is null or lower(p.country) = lower(:country)) " +
           "and (:religion is null or lower(p.religion) = lower(:religion)) " +
           "and (:activity is null or lower(p.specialties) like lower(concat('%', :activity, '%')))")
    List<Pandit> search(@Param("city") String city, @Param("country") String country,
                        @Param("religion") String religion, @Param("activity") String activity);
}
