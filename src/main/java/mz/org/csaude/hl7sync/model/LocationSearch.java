package mz.org.csaude.hl7sync.model;

import lombok.Data;

import java.util.List;

@Data
public class LocationSearch {
    private List<Location> results;
}
