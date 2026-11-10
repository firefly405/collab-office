package com.collaboffice.user;
import com.collaboffice.user.service.OrganizationService;
import com.collaboffice.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class OrganizationValidationTest {
 @Test void validatesAllFiveOperations(){assertDoesNotThrow(()->OrganizationService.permissions(Map.of("view",true,"edit",false,"download",false,"share",false,"delete",false)));assertThrows(BusinessException.class,()->OrganizationService.permissions(Map.of("view",true)));}
 @Test void rejectsWriteWithoutRead(){assertThrows(BusinessException.class,()->OrganizationService.permissions(Map.of("view",false,"edit",true,"download",false,"share",false,"delete",false)));}
}
