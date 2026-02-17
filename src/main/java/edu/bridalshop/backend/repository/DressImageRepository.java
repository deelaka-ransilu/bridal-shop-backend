package edu.bridalshop.backend.repository;

import edu.bridalshop.backend.entity.DressImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DressImageRepository extends JpaRepository<DressImage, Integer> {

    List<DressImage> findByDressDressIdOrderByDisplayOrderAsc(Integer dressId);

    void deleteByDressDressId(Integer dressId);
}