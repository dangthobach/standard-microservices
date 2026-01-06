/**
 * Standardized API Response Wrapper
 * Matches backend ApiResponse<T> structure
 */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
  errorCode?: string;
  error?: ErrorDetails;
  traceId?: string;
  spanId?: string;
  timestamp?: string;
}

export interface ErrorDetails {
  code?: string;
  message?: string;
  details?: any;
}

