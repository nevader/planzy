package pl.planzy.dto;


import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class SampleDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
}
