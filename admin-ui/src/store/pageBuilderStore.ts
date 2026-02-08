import { create } from 'zustand';
import { immer } from 'zustand/middleware/immer';
import { subscribeWithSelector } from 'zustand/middleware';
import { v4 as uuidv4 } from 'uuid';
import { PageBuilderState, PageSchema, SectionType, GridItem, StackItem, WidgetType } from '../types/pageBuilder';

const initialSchema: PageSchema = {
  id: uuidv4(),
  title: 'New Form',
  description: '',
  layout: 'classic',
  sections: [],
  theme: {
    primaryColor: '#1890ff',
    borderRadius: 6,
    spacing: 16
  },
  settings: {
    showProgress: true,
    allowBack: true,
    autoSave: false
  }
};

const initialState: PageBuilderState = {
  schema: initialSchema,
  selectedItemId: undefined,
  selectedSectionId: undefined,
  previewMode: false,
  draggedItem: undefined,
  history: [initialSchema],
  historyIndex: 0
};

interface PageBuilderStore extends PageBuilderState {
  // Actions
  setSchema: (schema: PageSchema) => void;
  addSection: (section: SectionType, index?: number) => void;
  updateSection: (sectionId: string, updates: Partial<SectionType>) => void;
  removeSection: (sectionId: string) => void;
  addItem: (sectionId: string, item: GridItem | StackItem) => void;
  updateItem: (sectionId: string, itemId: string, updates: Partial<GridItem | StackItem>) => void;
  removeItem: (sectionId: string, itemId: string) => void;
  moveItem: (fromSectionId: string, toSectionId: string, itemId: string, newPosition?: any) => void;
  selectItem: (itemId?: string, sectionId?: string) => void;
  setPreviewMode: (previewMode: boolean) => void;
  setDraggedItem: (draggedItem?: PageBuilderState['draggedItem']) => void;
  undo: () => void;
  redo: () => void;
  reset: () => void;
  
  // Utility methods
  addGridSection: (title?: string, cols?: number) => void;
  addStackSection: (title?: string, direction?: 'row' | 'column') => void;
  addWidgetToSection: (sectionId: string, widgetType: WidgetType, position?: any) => void;
  duplicateItem: (sectionId: string, itemId: string) => void;
  
  // History management
  saveToHistory: () => void;
  canUndo: () => boolean;
  canRedo: () => boolean;
}

export const usePageBuilderStore = create<PageBuilderStore>()(
  subscribeWithSelector(
    immer((set, get) => ({
      ...initialState,

      setSchema: (schema: PageSchema) => {
        set(state => {
          state.schema = schema;
          state.saveToHistory();
        });
      },

      addSection: (section: SectionType, index?: number) => {
        set(state => {
          if (index !== undefined) {
            state.schema.sections.splice(index, 0, section);
          } else {
            state.schema.sections.push(section);
          }
          state.selectedSectionId = section.id;
          state.saveToHistory();
        });
      },

      updateSection: (sectionId: string, updates: Partial<SectionType>) => {
        set(state => {
          const sectionIndex = state.schema.sections.findIndex(s => s.id === sectionId);
          if (sectionIndex !== -1) {
            Object.assign(state.schema.sections[sectionIndex], updates);
            state.saveToHistory();
          }
        });
      },

      removeSection: (sectionId: string) => {
        set(state => {
          state.schema.sections = state.schema.sections.filter(s => s.id !== sectionId);
          if (state.selectedSectionId === sectionId) {
            state.selectedSectionId = undefined;
          }
          state.saveToHistory();
        });
      },

      addItem: (sectionId: string, item: GridItem | StackItem) => {
        set(state => {
          const section = state.schema.sections.find(s => s.id === sectionId);
          if (section) {
            if (section.type === 'grid' && 'x' in item) {
              (section as any).items.push(item);
            } else if (section.type === 'stack' && 'order' in item) {
              (section as any).children.push(item);
            }
            state.selectedItemId = item.id;
            state.selectedSectionId = sectionId;
            state.saveToHistory();
          }
        });
      },

      updateItem: (sectionId: string, itemId: string, updates: Partial<GridItem | StackItem>) => {
        set(state => {
          const section = state.schema.sections.find(s => s.id === sectionId);
          if (section) {
            if (section.type === 'grid') {
              const itemIndex = (section as any).items.findIndex((item: GridItem) => item.id === itemId);
              if (itemIndex !== -1) {
                Object.assign((section as any).items[itemIndex], updates);
                state.saveToHistory();
              }
            } else if (section.type === 'stack') {
              const itemIndex = (section as any).children.findIndex((item: StackItem) => item.id === itemId);
              if (itemIndex !== -1) {
                Object.assign((section as any).children[itemIndex], updates);
                state.saveToHistory();
              }
            }
          }
        });
      },

      removeItem: (sectionId: string, itemId: string) => {
        set(state => {
          const section = state.schema.sections.find(s => s.id === sectionId);
          if (section) {
            if (section.type === 'grid') {
              (section as any).items = (section as any).items.filter((item: GridItem) => item.id !== itemId);
            } else if (section.type === 'stack') {
              (section as any).children = (section as any).children.filter((item: StackItem) => item.id !== itemId);
            }
            if (state.selectedItemId === itemId) {
              state.selectedItemId = undefined;
            }
            state.saveToHistory();
          }
        });
      },

      moveItem: (fromSectionId: string, toSectionId: string, itemId: string, newPosition?: any) => {
        set(state => {
          const fromSection = state.schema.sections.find(s => s.id === fromSectionId);
          const toSection = state.schema.sections.find(s => s.id === toSectionId);
          
          if (fromSection && toSection) {
            let item: GridItem | StackItem | undefined;
            
            // Remove from source section
            if (fromSection.type === 'grid') {
              const itemIndex = (fromSection as any).items.findIndex((i: GridItem) => i.id === itemId);
              if (itemIndex !== -1) {
                item = (fromSection as any).items.splice(itemIndex, 1)[0];
              }
            } else if (fromSection.type === 'stack') {
              const itemIndex = (fromSection as any).children.findIndex((i: StackItem) => i.id === itemId);
              if (itemIndex !== -1) {
                item = (fromSection as any).children.splice(itemIndex, 1)[0];
              }
            }
            
            // Add to target section
            if (item) {
              if (toSection.type === 'grid' && 'x' in item) {
                if (newPosition) {
                  (item as GridItem).x = newPosition.x;
                  (item as GridItem).y = newPosition.y;
                }
                (toSection as any).items.push(item);
              } else if (toSection.type === 'stack') {
                // Convert GridItem to StackItem if needed
                const stackItem: StackItem = {
                  id: item.id,
                  type: item.type,
                  props: item.props,
                  order: newPosition?.order || (toSection as any).children.length
                };
                (toSection as any).children.push(stackItem);
              }
              state.selectedSectionId = toSectionId;
              state.saveToHistory();
            }
          }
        });
      },

      selectItem: (itemId?: string, sectionId?: string) => {
        set(state => {
          state.selectedItemId = itemId;
          state.selectedSectionId = sectionId;
        });
      },

      setPreviewMode: (previewMode: boolean) => {
        set(state => {
          state.previewMode = previewMode;
          if (previewMode) {
            state.selectedItemId = undefined;
            state.selectedSectionId = undefined;
          }
        });
      },

      setDraggedItem: (draggedItem?: PageBuilderState['draggedItem']) => {
        set(state => {
          state.draggedItem = draggedItem;
        });
      },

      undo: () => {
        set(state => {
          if (state.historyIndex > 0) {
            state.historyIndex--;
            state.schema = structuredClone(state.history[state.historyIndex]);
          }
        });
      },

      redo: () => {
        set(state => {
          if (state.historyIndex < state.history.length - 1) {
            state.historyIndex++;
            state.schema = structuredClone(state.history[state.historyIndex]);
          }
        });
      },

      reset: () => {
        set(state => {
          Object.assign(state, initialState);
          state.schema = { ...initialSchema, id: uuidv4() };
          state.history = [state.schema];
          state.historyIndex = 0;
        });
      },

      // Utility methods
      addGridSection: (title = 'Grid Section', cols = 12) => {
        const section: SectionType = {
          id: uuidv4(),
          type: 'grid',
          title,
          cols,
          rowHeight: 60,
          items: [],
          breakpoints: {
            lg: 1200,
            md: 996,
            sm: 768,
            xs: 480
          }
        };
        get().addSection(section);
      },

      addStackSection: (title = 'Stack Section', direction = 'column' as const) => {
        const section: SectionType = {
          id: uuidv4(),
          type: 'stack',
          title,
          direction,
          align: 'stretch',
          justify: 'start',
          gap: 16,
          wrap: false,
          children: []
        };
        get().addSection(section);
      },

      addWidgetToSection: (sectionId: string, widgetType: WidgetType, position?: any) => {
        const section = get().schema.sections.find(s => s.id === sectionId);
        if (section) {
          if (section.type === 'grid') {
            const item: GridItem = {
              id: uuidv4(),
              type: widgetType,
              x: position?.x || 0,
              y: position?.y || 0,
              w: position?.w || 6,
              h: position?.h || 2,
              props: getDefaultPropsForWidget(widgetType),
              isDraggable: true,
              isResizable: true
            };
            get().addItem(sectionId, item);
          } else if (section.type === 'stack') {
            const item: StackItem = {
              id: uuidv4(),
              type: widgetType,
              props: getDefaultPropsForWidget(widgetType),
              order: (section as any).children.length
            };
            get().addItem(sectionId, item);
          }
        }
      },

      duplicateItem: (sectionId: string, itemId: string) => {
        const section = get().schema.sections.find(s => s.id === sectionId);
        if (section) {
          if (section.type === 'grid') {
            const item = (section as any).items.find((i: GridItem) => i.id === itemId);
            if (item) {
              const duplicatedItem: GridItem = {
                ...item,
                id: uuidv4(),
                x: (item as GridItem).x + 1,
                y: (item as GridItem).y + 1
              };
              get().addItem(sectionId, duplicatedItem);
            }
          } else if (section.type === 'stack') {
            const item = (section as any).children.find((i: StackItem) => i.id === itemId);
            if (item) {
              const duplicatedItem: StackItem = {
                ...item,
                id: uuidv4(),
                order: (item as StackItem).order + 1
              };
              get().addItem(sectionId, duplicatedItem);
            }
          }
        }
      },

      saveToHistory: () => {
        const currentState = get();
        const newHistory = currentState.history.slice(0, currentState.historyIndex + 1);
        newHistory.push(structuredClone(currentState.schema));
        
        // Keep history size manageable
        if (newHistory.length > 50) {
          newHistory.shift();
        } else {
          set(state => {
            state.historyIndex++;
          });
        }
        
        set(state => {
          state.history = newHistory;
        });
      },

      canUndo: () => get().historyIndex > 0,
      canRedo: () => get().historyIndex < get().history.length - 1
    }))
  )
);

// Helper function to get default props for widget types
function getDefaultPropsForWidget(widgetType: WidgetType): any {
  const defaultProps: Record<WidgetType, any> = {
    // Basic form fields
    'text-field': { label: 'Text Field', placeholder: 'Enter text...', required: false },
    'textarea': { label: 'Text Area', placeholder: 'Enter text...', rows: 4, required: false },
    'number': { label: 'Number', placeholder: 'Enter number...', min: 0, required: false },
    'email': { label: 'Email', placeholder: 'Enter email...', required: false },
    'password': { label: 'Password', placeholder: 'Enter password...', required: false },
    'select': { label: 'Select', placeholder: 'Choose option...', options: [{ label: 'Option 1', value: '1' }], required: false },
    'radio': { label: 'Radio Group', options: [{ label: 'Option 1', value: '1' }], required: false },
    'checkbox': { label: 'Checkbox Group', options: [{ label: 'Option 1', value: '1' }] },
    'date': { label: 'Date', placeholder: 'Select date...', required: false },
    'time': { label: 'Time', placeholder: 'Select time...', required: false },
    'switch': { label: 'Switch', checkedChildren: 'ON', unCheckedChildren: 'OFF' },
    'slider': { label: 'Slider', min: 0, max: 100, defaultValue: 50 },
    'rate': { label: 'Rate', allowHalf: true, character: '⭐' },
    'upload': { label: 'Upload', accept: '*', multiple: false, listType: 'text' },
    // Business fields
    'phone': { label: 'Phone', placeholder: 'Enter phone number...', required: false },
    'url': { label: 'URL', placeholder: 'Enter website URL...', required: false },
    'currency': { label: 'Currency', placeholder: 'Enter amount...', currency: 'USD', required: false },
    'percentage': { label: 'Percentage', placeholder: 'Enter percentage...', min: 0, max: 100, required: false },
    'signature': { label: 'Signature', placeholder: 'Please sign here...', required: false },
    'barcode': { label: 'Barcode', format: 'CODE128', displayValue: true },
    'qrcode': { label: 'QR Code', value: '', size: 128 },
    // Advanced fields
    'cascader': { label: 'Cascader', options: [], placeholder: 'Please select' },
    'tree-select': { label: 'Tree Select', treeData: [], placeholder: 'Please select' },
    'transfer': { label: 'Transfer', dataSource: [], titles: ['Source', 'Target'] },
    'mentions': { label: 'Mentions', placeholder: 'Type @ to mention...', required: false },
    'color-picker': { label: 'Color Picker', defaultValue: '#1890ff', showText: true },
    'code-editor': { label: 'Code Editor', language: 'javascript', theme: 'light', required: false },
    // Media & content
    'image': { src: '', alt: 'Image', width: '100%', height: 'auto' },
    'video': { src: '', controls: true, width: '100%', height: 'auto' },
    'audio': { src: '', controls: true, width: '100%' },
    'text-block': { content: 'Text block content', fontSize: 14, color: '#000' },
    'html': { content: '<div>Custom HTML content</div>', sanitize: true },
    'iframe': { src: '', width: '100%', height: '400px', title: 'Embedded content' },
    'document': { src: '', width: '100%', height: '600px' },
    // Layout & structure
    'divider': { orientation: 'center', dashed: false },
    'space': { size: 'middle', direction: 'horizontal' },
    'card': { title: 'Card Title', bordered: true, hoverable: false },
    'tabs': { defaultActiveKey: '1', items: [{ key: '1', label: 'Tab 1', children: 'Content 1' }] },
    'collapse': { defaultActiveKey: '1', items: [{ key: '1', label: 'Panel 1', children: 'Content 1' }] },
    'steps': { current: 0, items: [{ title: 'Step 1' }, { title: 'Step 2' }] },
    // Data display
    'list': { dataSource: [], renderItem: null },
    'table': { dataSource: [], columns: [] },
    'chart': { type: 'line', data: [], width: '100%', height: 300 },
    'timeline': { items: [{ children: 'Create a services site 2015-09-01' }] },
    'calendar': { mode: 'month', fullscreen: true },
    'kanban': { columns: ['To Do', 'In Progress', 'Done'], cards: [] }
  };
  
  return defaultProps[widgetType] || {};
}
