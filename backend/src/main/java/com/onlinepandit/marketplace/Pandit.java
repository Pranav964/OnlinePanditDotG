package com.onlinepandit.marketplace;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Table(name = "pandits")
public class Pandit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;
    @Column(name = "years_experience", nullable = false)
    private int yearsExperience;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Column(nullable = false, length = 80)
    private String city;
    @Column(length = 80)
    private String region;
    @Column(nullable = false, length = 80)
    private String country;
    @Column(nullable = false, length = 60)
    private String religion;
    @Column(length = 255)
    private String languages;
    @Column(length = 500)
    private String specialties;
    @Column(name = "image_url", length = 500)
    private String imageUrl;
    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;
    @Column(name = "review_count", nullable = false)
    private int reviewCount;
    @Column(nullable = false)
    private boolean approved;
    @Column(name = "user_id")
    private Long userId;
    protected Pandit() { }
    public Pandit(String fullName, int yearsExperience, LocalDate dateOfBirth, String city, String region,
                  String country, String religion, String languages, String specialties, Long userId) {
        this.fullName = fullName;
        this.yearsExperience = yearsExperience;
        this.dateOfBirth = dateOfBirth;
        this.city = city;
        this.region = region;
        this.country = country;
        this.religion = religion;
        this.languages = languages;
        this.specialties = specialties;
        this.rating = BigDecimal.valueOf(5.0);
        this.reviewCount = 0;
        this.approved = false;
        this.userId = userId;
    }
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public int getYearsExperience() { return yearsExperience; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getCity() { return city; }
    public String getRegion() { return region; }
    public String getCountry() { return country; }
    public String getReligion() { return religion; }
    public String getLanguages() { return languages; }
    public String getSpecialties() { return specialties; }
    public String getImageUrl() { return imageUrl; }
    public BigDecimal getRating() { return rating; }
    public int getReviewCount() { return reviewCount; }
    public boolean isApproved() { return approved; }
    public Long getUserId() { return userId; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public void recordReview(int reviewRating) {
        int updatedReviewCount = reviewCount + 1;
        rating = rating.multiply(BigDecimal.valueOf(reviewCount)).add(BigDecimal.valueOf(reviewRating))
                .divide(BigDecimal.valueOf(updatedReviewCount), 1, RoundingMode.HALF_UP);
        reviewCount = updatedReviewCount;
    }
}
