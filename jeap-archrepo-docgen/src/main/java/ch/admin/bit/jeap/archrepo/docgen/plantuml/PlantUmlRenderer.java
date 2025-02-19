package ch.admin.bit.jeap.archrepo.docgen.plantuml;

import ch.admin.bit.jeap.archrepo.docgen.ComponentContext;
import ch.admin.bit.jeap.archrepo.docgen.RelationView;
import ch.admin.bit.jeap.archrepo.docgen.SystemContext;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class PlantUmlRenderer {

    public String renderSystemContextView(SystemContext systemContext) {
        PlantUmlComponentView componentView = new PlantUmlComponentView();
        String systemName = systemContext.getSystem().getName();

        componentView.addFocusedComponent(systemName).removeLink();
        systemContext.getSystemsInContext()
                .forEach(name -> componentView.addComponent(name).removeLink());

        systemContext.getConsumedEventRelations().forEach(rel -> addIncomingRelation(componentView, rel, systemName));
        systemContext.getProducedEventRelations().forEach(rel -> addOutgoingRelation(componentView, rel, systemName));
        systemContext.getReceivedCommandRelations().forEach(rel -> addIncomingRelation(componentView, rel, systemName));
        systemContext.getSentCommandRelations().forEach(rel -> addOutgoingRelation(componentView, rel, systemName));
        systemContext.getProvidedRestApiRelations().forEach(rel -> addIncomingRelation(componentView, rel, systemName));
        systemContext.getConsumedRestApiRelations().forEach(rel -> addOutgoingRelation(componentView, rel, systemName));

        return componentView.render();
    }

    public String renderComponentContextView(ComponentContext componentContext) {
        PlantUmlComponentView componentView = new PlantUmlComponentView();
        String componentName = componentContext.getSystemComponent().getName();

        componentView.addFocusedComponent(componentName);
        componentContext.getComponentsInContext()
                .forEach(componentView::addComponent);

        componentContext.getConsumedEventRelations().forEach(rel -> addIncomingRelation(componentView, rel, componentName));
        componentContext.getProducedEventRelations().forEach(rel -> addOutgoingRelation(componentView, rel, componentName));
        componentContext.getReceivedCommandRelations().forEach(rel -> addIncomingRelation(componentView, rel, componentName));
        componentContext.getSentCommandRelations().forEach(rel -> addOutgoingRelation(componentView, rel, componentName));
        componentContext.getProvidedRestApiRelations().forEach(rel -> addIncomingRelation(componentView, rel, componentName));
        componentContext.getConsumedRestApiRelations().forEach(rel -> addOutgoingRelation(componentView, rel, componentName));

        return componentView.render();
    }

    void addIncomingRelation(PlantUmlComponentView view, RelationView relation, String target) {
        if (StringUtils.isEmpty(relation.getCounterpart())) {
            return;
        }
        view.addRelation(relation.getCounterpart(), target, relation.getType(), relation.getLabel());
    }

    void addOutgoingRelation(PlantUmlComponentView view, RelationView relation, String source) {
        // For self-relations (i.e. an event sent to itself), avoid rendering both as incoming and outgoing relation
        if (StringUtils.isEmpty(relation.getCounterpart()) || source.equals(relation.getCounterpart())) {
            return;
        }
        view.addRelation(source, relation.getCounterpart(), relation.getType(), relation.getLabel());
    }
}
