package com.collaboffice.search.repository;

import com.collaboffice.search.entity.DocumentIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import java.util.List;

public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndex, String> {
    List<DocumentIndex> findByTitleContainingOrContentContaining(String titleKeyword, String contentKeyword);
}
