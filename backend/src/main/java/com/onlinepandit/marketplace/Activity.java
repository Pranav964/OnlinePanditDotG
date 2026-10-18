package com.onlinepandit.marketplace;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "activities")
public class Activity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 80)
    private String category;
    @Column(nullable = false, length = 60)
    private String duration;
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;
    @Column(nullable = false, length = 60)
    private String religion;
    @Column(nullable = false)
    private boolean active;
    protected Activity() { }
    public Activity(String name, String category, String duration, BigDecimal basePrice, String religion) {
        this.name = name;
        this.category = category;
        this.duration = duration;
        this.basePrice = basePrice;
        this.religion = religion;
        this.active = true;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getDuration() { return duration; }
    public BigDecimal getBasePrice() { return basePrice; }
    public String getReligion() { return religion; }
}
