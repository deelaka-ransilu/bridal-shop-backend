package edu.bridalshop.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dress_images")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DressImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Integer imageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dress_id", nullable = false)
    private Dress dress;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
}