/*
 * The MIT License
 *
 * Copyright (c) 2016, CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.jenkinsci.plugins.pipeline.modeldefinition;

import hudson.triggers.TimerTrigger;
import hudson.triggers.Trigger;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TriggersTest extends AbstractModelDefTest {
    @Test
    public void simpleTriggers() throws Exception {
        WorkflowRun b = expect("simpleTriggers")
                .logContains("[Pipeline] { (foo)", "hello")
                .logNotContains("[Pipeline] { (Post Actions)")
                .go();

        WorkflowJob p = b.getParent();

        PipelineTriggersJobProperty triggersJobProperty = p.getTriggersJobProperty();
        assertNotNull(triggersJobProperty);
        assertEquals(1, triggersJobProperty.getTriggers().size());
        TimerTrigger.DescriptorImpl timerDesc = j.jenkins.getDescriptorByType(TimerTrigger.DescriptorImpl.class);

        Trigger trigger = triggersJobProperty.getTriggerForDescriptor(timerDesc);
        assertNotNull(trigger);

        assertTrue(trigger instanceof TimerTrigger);
        TimerTrigger timer = (TimerTrigger) trigger;
        assertEquals("@daily", timer.getSpec());
    }

    @Issue("JENKINS-44149")
    @Test
    public void triggersRemoved() throws Exception {
        WorkflowRun b = getAndStartNonRepoBuild("simpleTriggers");
        j.assertBuildStatusSuccess(j.waitForCompletion(b));

        WorkflowJob job = b.getParent();
        PipelineTriggersJobProperty triggersJobProperty = job.getProperty(PipelineTriggersJobProperty.class);
        assertNotNull(triggersJobProperty);
        assertEquals(1, triggersJobProperty.getTriggers().size());

        job.setDefinition(new CpsFlowDefinition(pipelineSourceFromResources("propsTriggersParamsRemoved"), true));
        WorkflowRun b2 = job.scheduleBuild2(0).waitForStart();
        j.assertBuildStatusSuccess(j.waitForCompletion(b2));

        assertNull(job.getProperty(PipelineTriggersJobProperty.class));
    }
}
