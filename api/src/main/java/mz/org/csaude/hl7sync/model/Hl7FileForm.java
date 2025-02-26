package mz.org.csaude.hl7sync.model;

import lombok.Data;


import java.util.List;

@Data
public class Hl7FileForm {

    private Location province;

    private Location district;

    private List<Location> healthFacilities;

    private int frequency;
    private String generationTime;
}
