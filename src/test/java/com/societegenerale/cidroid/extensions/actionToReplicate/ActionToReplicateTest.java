package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.actionToReplicate.ActionToReplicate;
import org.junit.Test;
import org.reflections.Reflections;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ActionToReplicateTest {

    @Test
    public void uiMethodsShouldReturnSomething() throws IllegalAccessException, InstantiationException {

        Reflections reflections = new Reflections("com.societegenerale.cidroid.extensions.actionToReplicate");

        Set<Class<? extends ActionToReplicate>> actionsToReplicateClasses = reflections.getSubTypesOf(ActionToReplicate.class);

        for (Class clazz : actionsToReplicateClasses) {

            ActionToReplicate actionToReplicate = (ActionToReplicate) clazz.newInstance();

            String className=actionToReplicate.getClass().getName();

            assertThat(actionToReplicate.getDescriptionForUI()).as(className+" should have a description").isNotBlank();
            assertThat(actionToReplicate.getExpectedUIFields()).as(className+" should have expected fields").isNotEmpty();
        }
    }
}
