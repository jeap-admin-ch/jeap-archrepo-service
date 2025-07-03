package ch.admin.bit.jeap.archrepo.web.rest.database;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseSchemaExceptionTest {

    @Test
    void schemaDeserializationError_withSystemAndComponentAndException_throwsExceptionWithCorrectMessageAndStatus() {
        Exception cause = new Exception("Deserialization error");
        DatabaseSchemaException exception = DatabaseSchemaException.schemaDeserializationError("TestSystem", "TestComponent", cause);

        assertEquals("Unable to deserialize the database schema for the component 'TestComponent' and the system 'TestSystem': Deserialization error", exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getResponseStatus());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void unexpectedError_withMockedDto_returnsExceptionWithCorrectMessageAndStatus() {
        CreateOrUpdateDbSchemaDto dto = mock(CreateOrUpdateDbSchemaDto.class);
        when(dto.getSystemComponentName()).thenReturn("MockComponent");

        DatabaseSchemaException exception = DatabaseSchemaException.unexpectedError(dto, "Some error");

        assertEquals("An unexpected error happened while processing the database schema for the component 'MockComponent': Some error.", exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getResponseStatus());
    }

    @Test
    void unexpectedError_withMockedDtoAndException_returnsExceptionWithCorrectMessageStatusAndCause() {
        CreateOrUpdateDbSchemaDto dto = mock(CreateOrUpdateDbSchemaDto.class);
        when(dto.getSystemComponentName()).thenReturn("MockComponent");
        Exception cause = new Exception("Unexpected failure");

        DatabaseSchemaException exception = DatabaseSchemaException.unexpectedError(dto, cause);

        assertEquals("An unexpected error happened while processing the database schema for the component 'MockComponent': Unexpected failure.", exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getResponseStatus());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void unexpectedError_withSystemComponentAndException_returnsExceptionWithCorrectMessageStatusAndCause() {
        Exception cause = new Exception("Component error");

        DatabaseSchemaException exception = DatabaseSchemaException.unexpectedError("ComponentX", cause);

        assertEquals("An unexpected error happened while processing the database schema for the component 'ComponentX': Component error.", exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getResponseStatus());
        assertEquals(cause, exception.getCause());
    }

    @Test
    void schemaSerializationError_withMockedDtoAndException_returnsExceptionWithCorrectMessageStatusAndCause() {
        CreateOrUpdateDbSchemaDto dto = mock(CreateOrUpdateDbSchemaDto.class);
        when(dto.getSystemComponentName()).thenReturn("SerializeComponent");
        Exception cause = new Exception("Serialization failed");

        DatabaseSchemaException exception = DatabaseSchemaException.schemaSerializationError(dto, cause);

        assertEquals("Unable to serialize the database schema for the component 'SerializeComponent': Serialization failed", exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getResponseStatus());
        assertEquals(cause, exception.getCause());
    }

    @ParameterizedTest
    @MethodSource("systemComponentDoesNotExistArguments")
    void systemComponentDoesNotExist_parameterized(String input, String expectedMessage) {
        DatabaseSchemaException exception = DatabaseSchemaException.systemComponentDoesNotExist(input);

        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, exception.getResponseStatus());
    }

    private static Stream<Arguments> systemComponentDoesNotExistArguments() {
        return Stream.of(
                Arguments.of(null, "The system component 'null' does not exist in the architecture model."),
                Arguments.of("", "The system component '' does not exist in the architecture model."),
                Arguments.of("ValidComponent", "The system component 'ValidComponent' does not exist in the architecture model."),
                Arguments.of("TestComponent", "The system component 'TestComponent' does not exist in the architecture model.")
        );
    }


}