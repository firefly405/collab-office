package com.collaboffice.common.security;

import com.collaboffice.common.config.WebMvcConfig;
import com.collaboffice.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.*;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlatformInterceptorTest {
    static class Registry extends InterceptorRegistry {List<Object> entries(){return getInterceptors();}}
    @Test void mvcActuallyRegistersAdministrativePaths(){
        var factory=new StaticListableBeanFactory(Map.of("access",mock(WorkspaceAccess.class)));
        var registry=new Registry();new WebMvcConfig(factory.getBeanProvider(WorkspaceAccess.class)).addInterceptors(registry);
        var resource=(MappedInterceptor)registry.entries().stream().filter(i->i instanceof MappedInterceptor m && m.getInterceptor() instanceof ResourceAccessInterceptor).findFirst().orElseThrow();
        for(var path:List.of("/api/v1/admin/dashboard/stats","/api/v1/space/create","/api/v1/org/dept")){
            var request=new MockHttpServletRequest("POST",path);ServletRequestPathUtils.parseAndCache(request);
            assertTrue(resource.matches(request),path);
        }
    }
    @Test void systemAndTeamMutationsRequireSystemRole(){
        var access=mock(WorkspaceAccess.class);var interceptor=new ResourceAccessInterceptor(access);
        doThrow(new BusinessException(403,"Forbidden")).when(access).requireSystemAdmin();
        for(var path:List.of("/api/v1/admin/dashboard/stats","/api/v1/space/create","/api/v1/space/15/members")){
            var request=new MockHttpServletRequest(path.contains("/admin/")?"GET":"POST",path);
            assertThrows(BusinessException.class,()->interceptor.preHandle(request,new MockHttpServletResponse(),new Object()));
        }
    }
    @Test void documentOwnersStillUseDocumentGrantEndpoint(){
        var access=mock(WorkspaceAccess.class);
        assertTrue(new ResourceAccessInterceptor(access).preHandle(new MockHttpServletRequest("PUT","/api/v1/space/15/organization/documents/100"),new MockHttpServletResponse(),new Object()));
        verify(access,never()).requireSystemAdmin();
    }
}
