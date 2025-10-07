{{- define "retrieval-service.fullname" -}}
{{- printf "%s-%s" .Release.Name "retrieval" | trunc 63 | trimSuffix "-" -}}
{{- end -}}
