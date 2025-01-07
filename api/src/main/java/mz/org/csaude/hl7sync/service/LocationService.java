package mz.org.csaude.hl7sync.service;

import mz.org.csaude.hl7sync.model.Location;

import java.util.List;

public interface LocationService {

    Location findByUuid(String uuid);

    List<Location> findAllProvinces();
}
