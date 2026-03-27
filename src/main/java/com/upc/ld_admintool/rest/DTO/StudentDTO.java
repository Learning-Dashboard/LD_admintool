package com.upc.ld_admintool.rest.DTO;
import java.util.Map;
import lombok.Data;
import com.upc.ld_admintool.domain.utils.DataSource;

@Data
public class StudentDTO {
    private Long id;
    private String name;
    private Map<DataSource, StudentIdentityDTO> identities;
    private ProjectDTO project;
}
