package com.collaboffice.user;
import com.collaboffice.user.service.*;
import com.collaboffice.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class PreferenceValidationTest {
 @Test void acceptsDefaultsAndRejectsUnboundedEditor(){var values=PreferenceService.defaults();assertDoesNotThrow(()->PreferenceService.validate(values));values.put("editorSize",100);assertThrows(BusinessException.class,()->PreferenceService.validate(values));}
 @Test void rejectsUnsupportedNotificationAndNavigation(){var values=PreferenceService.defaults();values.put("notifications",Map.of("collab","false"));assertThrows(BusinessException.class,()->PreferenceService.validate(values));var bad=PreferenceService.defaults();bad.put("defaultView","https://outside.test");assertThrows(BusinessException.class,()->PreferenceService.validate(bad));}
 @Test void onlyReadsConfiguredBucket(){assertEquals("demo/a.txt",WorkspaceDataService.objectName("http://127.0.0.1:9000/doc-files/demo/a.txt","doc-files"));assertThrows(BusinessException.class,()->WorkspaceDataService.objectName("http://outside.test/private/a","doc-files"));}
}
