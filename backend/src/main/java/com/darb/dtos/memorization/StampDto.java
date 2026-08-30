package com.darb.dtos.memorization;

import com.darb.entities.enums.StampType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StampDto {

    @NotNull
    private StampType type;

    @NotNull
    @Min(1)
    @Max(114)
    private Integer surah;

    @NotNull
    @Min(1)
    private Integer ayah;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double x;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private Double y;
}
