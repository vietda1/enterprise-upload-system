import { useState, useCallback } from 'react';
import uploadService from '../services/upload.service';
import validationService from '../services/validation.service';
import ingestService from '../services/ingest.service';

export const useUpload = () => {
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState({});
  const [errors, setErrors] = useState({});

  const uploadFile = useCallback(async (file, metadata) => {
    const fileId = Date.now() + Math.random();
    
    try {
      setUploading(true);
      setProgress(prev => ({ ...prev, [fileId]: 0 }));

      // Step 1: Get presigned URL
      const { uploadId, presignedUrl, objectKey } = 
        await uploadService.getPresignedUrl(file.name, file.type, metadata);

      // Step 2: Upload to MinIO
      await uploadService.uploadToMinio(
        presignedUrl,
        file,
        (progress) => {
          setProgress(prev => ({ ...prev, [fileId]: progress }));
        }
      );

      // Step 3: Confirm upload
      await uploadService.confirmUpload(uploadId);

      // Step 4: Poll for validation status
      const validationResult = await pollValidationStatus(uploadId);

      if (validationResult.status === 'VALID') {
        // Step 5: Trigger ingestion if auto-ingest enabled
        if (metadata.autoIngest) {
          await ingestService.triggerIngestion(uploadId);
        }
      }

      return { uploadId, status: 'SUCCESS' };
    } catch (error) {
      setErrors(prev => ({ 
        ...prev, 
        [fileId]: error.response?.data?.message || error.message 
      }));
      throw error;
    } finally {
      setUploading(false);
    }
  }, []);

  const pollValidationStatus = async (uploadId, maxAttempts = 30) => {
    for (let i = 0; i < maxAttempts; i++) {
      await new Promise(resolve => setTimeout(resolve, 2000));
      const result = await validationService.getValidationResult(uploadId);
      
      if (result.status !== 'PENDING') {
        return result;
      }
    }
    throw new Error('Validation timeout');
  };

  return { uploadFile, uploading, progress, errors };
};