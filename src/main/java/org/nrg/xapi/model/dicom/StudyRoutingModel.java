package org.nrg.xapi.model.dicom;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class StudyRoutingModel implements Serializable {
    private static final long serialVersionUID = -2489157896180259236L;

    private String studyInstanceUid;
    private String project;
    private String subject;
    private String label;
    private String user;
    private String created;
    private String accessed;
}
