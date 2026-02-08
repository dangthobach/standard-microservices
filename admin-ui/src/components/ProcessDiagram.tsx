import React, { useEffect, useRef } from 'react';
import BpmnViewer from 'bpmn-js/lib/Viewer';
import { Card, Spin, message } from 'antd';

interface ProcessDiagramProps {
  bpmnXml: string;
  highlightedActivities?: string[];
  completedActivities?: string[];
  currentActivity?: string;
}

const ProcessDiagram: React.FC<ProcessDiagramProps> = ({
  bpmnXml,
  highlightedActivities = [],
  completedActivities = [],
  currentActivity,
}) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewerRef = useRef<BpmnViewer | null>(null);

  const addActivityOverlays = React.useCallback((
    viewer: BpmnViewer,
    highlighted: string[],
    completed: string[],
    current?: string
  ) => {
    const canvas = viewer.get('canvas') as any;
    const elementRegistry = viewer.get('elementRegistry') as any;

    // Add overlays for completed activities
    completed.forEach(activityId => {
      const element = elementRegistry.get(activityId);
      if (element) {
        canvas.addMarker(element.id, 'completed');
        addOverlay(viewer, element, '✓', 'completed-overlay');
      }
    });

    // Add overlays for highlighted activities
    highlighted.forEach(activityId => {
      const element = elementRegistry.get(activityId);
      if (element) {
        canvas.addMarker(element.id, 'highlighted');
        addOverlay(viewer, element, '→', 'highlighted-overlay');
      }
    });

    // Add overlay for current activity
    if (current) {
      const element = elementRegistry.get(current);
      if (element) {
        canvas.addMarker(element.id, 'current');
        addOverlay(viewer, element, '●', 'current-overlay');
      }
    }
  }, []);

  useEffect(() => {
    if (!containerRef.current || !bpmnXml) return;

    // Initialize BPMN viewer
    const viewer = new BpmnViewer({
      container: containerRef.current,
      width: '100%',
      height: '600px',
    });

    viewerRef.current = viewer;

    // Load BPMN XML
    viewer
      .importXML(bpmnXml)
      .then(() => {
        // Fit to view
        (viewer.get('canvas') as any).zoom('fit-viewport');
        
        // Add overlays for highlighting
        addActivityOverlays(viewer, highlightedActivities, completedActivities, currentActivity);
      })
      .catch((err: any) => {
        message.error('Failed to load BPMN diagram');
        console.error('Error loading BPMN:', err);
      });

    return () => {
      if (viewerRef.current) {
        viewerRef.current.destroy();
      }
    };
  }, [bpmnXml, highlightedActivities, completedActivities, currentActivity, addActivityOverlays]);

  const addOverlay = (viewer: BpmnViewer, element: any, text: string, className: string) => {
    const canvas = viewer.get('canvas') as any;
    const overlay = document.createElement('div');
    overlay.className = className;
    overlay.textContent = text;
    overlay.style.cssText = `
      position: absolute;
      background: #1890ff;
      color: white;
      border-radius: 50%;
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: bold;
      z-index: 1000;
    `;
    
    canvas.addOverlay(element.id, {
      position: { top: -10, left: -10 },
      html: overlay,
    });
  };

  if (!bpmnXml) {
    return (
      <Card title="Process Diagram">
        <div style={{ textAlign: 'center', padding: '50px' }}>
          <Spin size="large" />
          <p>Loading diagram...</p>
        </div>
      </Card>
    );
  }

  return (
    <Card title="Process Diagram">
      <div
        ref={containerRef}
        style={{
          width: '100%',
          height: '600px',
          border: '1px solid #d9d9d9',
          borderRadius: '6px',
        }}
      />
      
      {/* Legend */}
      <div style={{ marginTop: '16px', display: 'flex', gap: '16px', flexWrap: 'wrap' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div style={{ width: '20px', height: '20px', background: '#52c41a', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: '12px' }}>✓</div>
          <span>Completed</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div style={{ width: '20px', height: '20px', background: '#1890ff', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: '12px' }}>→</div>
          <span>Highlighted</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <div style={{ width: '20px', height: '20px', background: '#faad14', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: '12px' }}>●</div>
          <span>Current</span>
        </div>
      </div>
    </Card>
  );
};

export default ProcessDiagram;
