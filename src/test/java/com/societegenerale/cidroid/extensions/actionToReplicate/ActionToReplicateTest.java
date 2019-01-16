package com.societegenerale.cidroid.extensions.actionToReplicate;

import com.societegenerale.cidroid.api.actionToReplicate.ActionToReplicate;
import org.junit.Test;
import org.reflections.Reflections;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ActionToReplicateTest {

    private  Reflections reflections = new Reflections("com.societegenerale.cidroid.extensions.actionToReplicate");

    private Set<Class<? extends ActionToReplicate>> actionsToReplicateClasses = reflections.getSubTypesOf(ActionToReplicate.class);

    @Test
    public void shouldHaveExpectedUIFields() throws IllegalAccessException, InstantiationException {

        List<Class> exceptions = Arrays.asList(DeleteResourceAction.class);

        for (Class clazz : actionsToReplicateClasses) {

            if (!exceptions.contains(clazz)) {

                ActionToReplicate actionToReplicate = (ActionToReplicate) clazz.newInstance();

                assertThat(actionToReplicate.getExpectedUIFields()).as(actionToReplicate.getClass().getName() + " should have expected fields").isNotEmpty();
            }
        }
    }

    @Test
    public void shouldHaveDescriptionForUI() throws IllegalAccessException, InstantiationException {

        for (Class clazz : actionsToReplicateClasses) {
            ActionToReplicate actionToReplicate = (ActionToReplicate) clazz.newInstance();

            assertThat(actionToReplicate.getDescriptionForUI()).as(actionToReplicate.getClass().getName() + " should have a description").isNotBlank();
        }
    }
}
