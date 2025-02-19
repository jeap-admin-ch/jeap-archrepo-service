package ch.admin.bit.jeap.archrepo.docgen;

import java.util.Set;

public interface ConfluenceAdapter {
    /**
     * @return Page ID
     */
    String getPageByName(String pageName);

    /**
     * @return Page ID
     */
    String addOrUpdatePageUnderAncestor(String ancestorId, String pageName, String content);

    /**
     * Deletes all child pages under rootPageId if the child page ID is not contained in generatedPageIds
     *
     * @return Deleted page count
     */
    int deleteOrphanPages(String rootPageId, Set<String> generatedPageIds);
}