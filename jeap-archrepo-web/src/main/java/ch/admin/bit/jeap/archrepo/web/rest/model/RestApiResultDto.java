package ch.admin.bit.jeap.archrepo.web.rest.model;

import ch.admin.bit.jeap.archrepo.persistence.ApiDocDto;

import java.time.ZonedDateTime;
import java.util.List;

public record RestApiResultDto (String serverUrl, ZonedDateTime lastUpdated, String version, List<RestApiDto> restApis){

    public static RestApiResultDto of(ApiDocDto apiDocDto, List<RestApiDto> restApis) {
        return new RestApiResultDto(apiDocDto.getServerUrl(), getLastUploadTime(apiDocDto), apiDocDto.getVersion(), restApis);
    }

    private static ZonedDateTime getLastUploadTime(ApiDocDto apiDocVersion) {
        return apiDocVersion.getModifiedAt() != null ? apiDocVersion.getModifiedAt() : apiDocVersion.getCreatedAt();
    }
}
