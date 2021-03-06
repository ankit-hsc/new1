/*
Copyright 2017-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License").
You may not use this file except in compliance with the License.
A copy of the License is located at

   http://aws.amazon.com/apache2.0/

or in the "license" file accompanying this file.
This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.
*/
package com.tensor.utilities;

import com.tensor.ebml.MkvTypeInfos;
import com.tensor.mkv.Frame;
import com.tensor.mkv.MkvElementVisitor;
import com.tensor.mkv.MkvValue;
import com.tensor.mkv.visitors.CompositeMkvElementVisitor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;

@Slf4j
public class FrameVisitor extends CompositeMkvElementVisitor {
    private final FragmentMetadataVisitor fragmentMetadataVisitor;
    private final FrameVisitorInternal frameVisitorInternal;
    private final FrameProcessor frameProcessor;

    private FrameVisitor(FragmentMetadataVisitor fragmentMetadataVisitor, FrameProcessor frameProcessor) {
        super(fragmentMetadataVisitor);
        this.fragmentMetadataVisitor = fragmentMetadataVisitor;
        this.frameVisitorInternal = new FrameVisitorInternal();
        this.childVisitors.add(this.frameVisitorInternal);
        this.frameProcessor = frameProcessor;
    }

    public static FrameVisitor create(FrameProcessor frameProcessor) {
        return new FrameVisitor(FragmentMetadataVisitor.create(), frameProcessor);
    }

    public interface FrameProcessor {
        default void process(Frame frame, MkvTrackMetadata trackMetadata) {
            throw new NotImplementedException("Default FrameVisitor.FrameProcessor");
        }
    }

    private class FrameVisitorInternal extends MkvElementVisitor {
        @Override
        public void visit(com.tensor.mkv.MkvStartMasterElement startMasterElement)
                throws com.tensor.mkv.MkvElementVisitException {
        }

        @Override
        public void visit(com.tensor.mkv.MkvEndMasterElement endMasterElement)
                throws com.tensor.mkv.MkvElementVisitException {
        }

        @Override
        public void visit(com.tensor.mkv.MkvDataElement dataElement)
                throws com.tensor.mkv.MkvElementVisitException {

            if (MkvTypeInfos.SIMPLEBLOCK.equals(dataElement.getElementMetaData().getTypeInfo())) {
                MkvValue<Frame> frame = dataElement.getValueCopy();
                Validate.notNull(frame);
                MkvTrackMetadata trackMetadata =
                        fragmentMetadataVisitor.getMkvTrackMetadata(frame.getVal().getTrackNumber());
                frameProcessor.process(frame.getVal(), trackMetadata);
            }
        }
    }
}
