{{- define "kong.fullname" -}}
{{- printf "%s-%s" .Release.Name "kong" | trunc 63 | trimSuffix "-" -}}
{{- end -}}
