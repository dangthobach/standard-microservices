// Modern Page Builder Types

export type WidgetType = 
  // Basic form fields
  | 'text-field'
  | 'textarea'
  | 'number'
  | 'email'
  | 'password'
  | 'select'
  | 'radio'
  | 'checkbox'
  | 'date'
  | 'time'
  | 'switch'
  | 'slider'
  | 'rate'
  | 'upload'
  // Business fields
  | 'phone'
  | 'url'
  | 'currency'
  | 'percentage' 
  | 'signature'
  | 'barcode'
  | 'qrcode'
  // Advanced fields
  | 'cascader'
  | 'tree-select'
  | 'transfer'
  | 'mentions'
  | 'color-picker'
  | 'code-editor'
  // Media & content
  | 'image'
  | 'video'
  | 'audio'
  | 'text-block'
  | 'html'
  | 'iframe'
  | 'document'
  // Layout & structure
  | 'divider'
  | 'space'
  | 'card'
  | 'tabs'
  | 'collapse'
  | 'steps'
  // Data display
  | 'list'
  | 'table'
  | 'chart'
  | 'timeline'
  | 'calendar'
  | 'kanban';

export interface WidgetProps {
  [key: string]: any;
}

export interface GridItem {
  id: string;
  type: WidgetType;
  x: number;
  y: number;
  w: number;
  h: number;
  props: WidgetProps;
  minW?: number;
  minH?: number;
  maxW?: number;
  maxH?: number;
  static?: boolean;
  isDraggable?: boolean;
  isResizable?: boolean;
}

export interface StackItem {
  id: string;
  type: WidgetType;
  props: WidgetProps;
  order: number;
}

export interface GridSection {
  id: string;
  type: 'grid';
  title?: string;
  cols: number;
  rowHeight: number;
  items: GridItem[];
  breakpoints?: {
    lg: number;
    md: number;
    sm: number;
    xs: number;
  };
}

export interface StackSection {
  id: string;
  type: 'stack';
  title?: string;
  direction: 'row' | 'column';
  align?: 'start' | 'center' | 'end' | 'stretch';
  justify?: 'start' | 'center' | 'end' | 'space-between' | 'space-around' | 'space-evenly';
  gap?: number;
  wrap?: boolean;
  children: StackItem[];
}

export type SectionType = GridSection | StackSection;

export interface PageSchema {
  id: string;
  title: string;
  description?: string;
  layout: 'classic' | 'card';
  sections: SectionType[];
  theme?: {
    primaryColor?: string;
    borderRadius?: number;
    spacing?: number;
  };
  settings?: {
    showProgress?: boolean;
    allowBack?: boolean;
    autoSave?: boolean;
  };
}

export interface FormData {
  [fieldId: string]: any;
}

export interface ValidationRule {
  required?: boolean;
  pattern?: string;
  min?: number;
  max?: number;
  message?: string;
}

export interface FieldValidation {
  [fieldId: string]: ValidationRule[];
}

// Page Builder State
export interface PageBuilderState {
  schema: PageSchema;
  selectedItemId?: string;
  selectedSectionId?: string;
  previewMode: boolean;
  draggedItem?: {
    type: 'widget' | 'item';
    widgetType?: WidgetType;
    fromPalette?: boolean;
    data?: any;
  };
  history: PageSchema[];
  historyIndex: number;
}

// Actions
export type PageBuilderAction =
  | { type: 'SET_SCHEMA'; payload: PageSchema }
  | { type: 'ADD_SECTION'; payload: { section: SectionType; index?: number } }
  | { type: 'UPDATE_SECTION'; payload: { sectionId: string; updates: Partial<SectionType> } }
  | { type: 'REMOVE_SECTION'; payload: { sectionId: string } }
  | { type: 'ADD_ITEM'; payload: { sectionId: string; item: GridItem | StackItem } }
  | { type: 'UPDATE_ITEM'; payload: { sectionId: string; itemId: string; updates: Partial<GridItem | StackItem> } }
  | { type: 'REMOVE_ITEM'; payload: { sectionId: string; itemId: string } }
  | { type: 'MOVE_ITEM'; payload: { fromSectionId: string; toSectionId: string; itemId: string; newPosition?: any } }
  | { type: 'SELECT_ITEM'; payload: { itemId?: string; sectionId?: string } }
  | { type: 'SET_PREVIEW_MODE'; payload: { previewMode: boolean } }
  | { type: 'SET_DRAGGED_ITEM'; payload: { draggedItem?: PageBuilderState['draggedItem'] } }
  | { type: 'UNDO' }
  | { type: 'REDO' }
  | { type: 'RESET' };

// Widget Configuration
export interface WidgetConfig {
  type: WidgetType;
  title: string;
  icon: string;
  category: 'form' | 'layout' | 'display' | 'data';
  description: string;
  defaultProps: WidgetProps;
  defaultSize: { w: number; h: number };
  minSize?: { w: number; h: number };
  maxSize?: { w: number; h: number };
  configurable: string[]; // Array of prop keys that can be configured
  isFormField: boolean;
}
