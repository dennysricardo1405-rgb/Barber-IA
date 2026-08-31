package com.example.BarberiaLaClasica.repository;

import com.example.BarberiaLaClasica.model.SliderImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SliderImageRepository extends JpaRepository<SliderImage, Long> {
    List<SliderImage> findByActivoTrueOrderByOrdenAsc();
    List<SliderImage> findAllByOrderByOrdenAsc();
}