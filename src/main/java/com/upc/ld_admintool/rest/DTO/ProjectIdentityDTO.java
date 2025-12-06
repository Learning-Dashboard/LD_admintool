package com.upc.ld_admintool.rest.DTO;
import com.upc.ld_admintool.domain.utils.DataSource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectIdentityDTO {
    private DataSource dataSource;
    private String url;
    private ProjectDTO project;
}
