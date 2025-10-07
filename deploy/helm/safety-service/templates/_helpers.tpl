{{- define "safety-service.fullname" -}}
{{- printf "%s-%s" .Release.Name "safety" | trunc 63 | trimSuffix "-" -}}
{{- end -}}
