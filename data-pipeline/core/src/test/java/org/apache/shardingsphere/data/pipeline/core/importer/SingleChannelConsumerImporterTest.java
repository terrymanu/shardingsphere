/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.data.pipeline.core.importer;

import org.apache.shardingsphere.data.pipeline.core.channel.PipelineChannel;
import org.apache.shardingsphere.data.pipeline.core.importer.sink.PipelineSink;
import org.apache.shardingsphere.data.pipeline.core.ingest.position.type.finished.IngestFinishedPosition;
import org.apache.shardingsphere.data.pipeline.core.ingest.record.DataRecord;
import org.apache.shardingsphere.data.pipeline.core.ingest.record.FinishedRecord;
import org.apache.shardingsphere.data.pipeline.core.ingest.record.Record;
import org.apache.shardingsphere.data.pipeline.core.job.progress.listener.PipelineJobProgressListener;
import org.apache.shardingsphere.data.pipeline.core.job.progress.listener.PipelineJobUpdateProgress;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SingleChannelConsumerImporterTest {
    
    @Test
    void assertStartConfirmsTailAfterWrite() {
        PipelineChannel channel = mock(PipelineChannel.class);
        PipelineSink sink = mock(PipelineSink.class);
        PipelineJobProgressListener listener = mock(PipelineJobProgressListener.class);
        List<Record> records = Arrays.asList(mock(DataRecord.class), new FinishedRecord(new IngestFinishedPosition()));
        PipelineJobUpdateProgress progress = new PipelineJobUpdateProgress(1);
        when(channel.fetch(100000, 3000L)).thenReturn(records);
        when(sink.write("", records)).thenReturn(progress);
        new SingleChannelConsumerImporter(channel, 100000, 3000L, sink, listener).start();
        InOrder order = inOrder(sink, channel, listener);
        order.verify(sink).write("", records);
        order.verify(channel).ack(records);
        order.verify(listener).onProgressUpdated(progress);
    }
    
    @Test
    void assertStartDoesNotConfirmFailedWrite() {
        PipelineChannel channel = mock(PipelineChannel.class);
        PipelineSink sink = mock(PipelineSink.class);
        PipelineJobProgressListener listener = mock(PipelineJobProgressListener.class);
        List<Record> records = Collections.singletonList(new FinishedRecord(new IngestFinishedPosition()));
        when(channel.fetch(100000, 3000L)).thenReturn(records);
        when(sink.write("", records)).thenThrow(new IllegalStateException("foo_write_failed"));
        SingleChannelConsumerImporter importer = new SingleChannelConsumerImporter(channel, 100000, 3000L, sink, listener);
        assertThrows(IllegalStateException.class, importer::start);
        verify(channel, never()).ack(records);
        verify(listener, never()).onProgressUpdated(any());
    }
}
