package com.upc.ld_admintool.rest.DTO;

import com.upc.ld_admintool.domain.utils.DataSource;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class ProjectDTO {
    private Long id;
    private String externalId;
    private String name;
    private String description;
    private byte[] logo;
    private boolean active;
    private String backlogId;
    private Boolean isGlobal;
    private boolean anonymized;
    private String subject;

    private String githubToken;

    private Map<DataSource, ProjectIdentityDTO> identities;
    private List<StudentDTO> students;
}
