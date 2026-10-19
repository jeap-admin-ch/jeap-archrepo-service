package ch.admin.bit.jeap.archrepo.docgen;

import java.util.Set;

public interface ConfluenceAdapter {

    /**
     * @return Page ID
     */
    String findOrCreatePageUnderAncestor(String ancestorId, String pageName);

    void updatePage(String pageId, String ancestorId, String pageName, String content);

    /**
     * Deletes all child pages under rootPageId if the child page ID is not contained in generatedPageIds
     *
     * @return Deleted page count
     */
    int deleteOrphanPages(String rootPageId, Set<String> generatedPageIds);
}
