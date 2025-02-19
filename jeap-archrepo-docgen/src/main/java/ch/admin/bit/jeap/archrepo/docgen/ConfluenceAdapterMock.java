package ch.admin.bit.jeap.archrepo.docgen;

import lombok.extern.slf4j.Slf4j;

import java.util.Set;

@Slf4j
public class ConfluenceAdapterMock implements ConfluenceAdapter {

    @Override
    public String getPageByName(String pageName) {
        int fakePageId = pageName.hashCode();
        log.info("Get page by name: pageName={} fakePageId={}", pageName, fakePageId);
        return String.valueOf(fakePageId);
    }

    @Override
    public String addOrUpdatePageUnderAncestor(String ancestorId, String pageName, String content) {
        int fakePageid = (ancestorId + pageName).hashCode();
        log.info("Add or update page: ancestorId={} pageName={} fakePageId={}", ancestorId, pageName, fakePageid);
        return String.valueOf(fakePageid);
    }

    @Override
    public int deleteOrphanPages(String rootPageId, Set<String> generatedPageIds) {
        return 0;
    }
}
