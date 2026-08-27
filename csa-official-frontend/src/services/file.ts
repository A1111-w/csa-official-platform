import api from "@/lib/axios"

export const fileService = {
  upload: async (file: File) => {
    const formData = new FormData()
    formData.append("file", file)

    return api.post<string, string>("/api/common/file/upload", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    })
  },
}
