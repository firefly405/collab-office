package com.collaboffice.search.listener;

import com.collaboffice.common.config.JacksonConfig;
import com.collaboffice.search.entity.DocumentIndex;
import com.collaboffice.search.entity.TaskIndex;
import com.collaboffice.search.repository.DocumentIndexRepository;
import com.collaboffice.search.repository.TaskIndexRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SearchIndexingListenerTest {
    private final DocumentIndexRepository documents=mock(DocumentIndexRepository.class);
    private final TaskIndexRepository tasks=mock(TaskIndexRepository.class);
    private final SearchIndexingListener listener=new SearchIndexingListener(documents,tasks,new JacksonConfig().objectMapper());

    @Test void indexesDocumentEventContainingAction() {
        listener.indexDocument(Map.of("id","123","action","UPDATE","title","Planning","spaceId",1L));
        var capture=ArgumentCaptor.forClass(DocumentIndex.class);
        verify(documents).save(capture.capture());
        assertEquals("Planning",capture.getValue().getTitle());
    }
    @Test void deletesDocumentWithoutRequiringContent() {
        listener.indexDocument(Map.of("id","123","action","DELETE"));
        verify(documents).deleteById("123");
        verifyNoMoreInteractions(documents);
    }
    @Test void indexesTaskEventContainingAction() {
        listener.indexTask(Map.of("id","456","action","UPDATE","title","Review","status",1));
        var capture=ArgumentCaptor.forClass(TaskIndex.class);
        verify(tasks).save(capture.capture());
        assertEquals(1,capture.getValue().getStatus());
    }
    @Test void deletesTaskWithoutRequiringContent() {
        listener.indexTask(Map.of("id","456","action","DELETE"));
        verify(tasks).deleteById("456");
        verifyNoMoreInteractions(tasks);
    }
}
