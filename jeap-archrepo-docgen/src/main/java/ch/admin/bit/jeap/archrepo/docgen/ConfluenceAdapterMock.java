package ch.admin.bit.jeap.archrepo.docgen;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class ConfluenceAdapterMock implements ConfluenceAdapter {

    @Override
    public String findOrCreatePageUnderAncestor(String ancestorId, String pageName) {
        int fakePageid = (ancestorId + pageName).hashCode();
        log.info("Find or create page: ancestorId={} pageName={} fakePageId={}", ancestorId, pageName, fakePageid);
        return String.valueOf(fakePageid);
    }

    @Override
    public void updatePage(String pageId, String ancestorId, String pageName, String content) {
        log.info("Update page: pageId={} ancestorId={} pageName={}", pageId, ancestorId, pageName);
    }

    @Override
    public int deleteOrphanPages(String rootPageId, Set<String> generatedPageIds) {
        return 0;
    }
}
