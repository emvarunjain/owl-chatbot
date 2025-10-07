{{- define "model-proxy.fullname" -}}
{{- printf "%s-%s" .Release.Name "modelproxy" | trunc 63 | trimSuffix "-" -}}
{{- end -}}
