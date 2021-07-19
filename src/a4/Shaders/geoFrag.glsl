#version 430

in vec2 tcG;
in vec3 varyingNormalG;
in vec3 varyingLightDirG;
in vec3 varyingVertPosG;
in vec3 varyingHalfVectorG;
in vec4 shadow_coord;
in vec3 vertEyeSpacePos;
out vec4 fragColor;

struct PositionalLight
{	vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    vec3 position;
};
struct Material
{	vec4 ambient;
    vec4 diffuse;
    vec4 specular;
    float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix;
uniform int enableLighting;

layout (binding=0) uniform sampler2D samp;
layout (binding=1) uniform sampler2DShadow shadowTex;
layout (binding=2) uniform sampler2D height;

void main(void)
{
    // normalized the light, normal, and eye direction vectors
    vec3 L = normalize(varyingLightDirG);
    vec3 N = normalize(varyingNormalG);

    //FOG Stuff
    vec4 fog = vec4(0.7, 0.8, 0.9, 1.0);	//Color
    float fogStart = 0.2;
    float fogEnd = 800;
    float dist = length(vertEyeSpacePos.xyz);
    float fogFactor = clamp(((fogEnd-dist)/(fogEnd - fogStart)), 0.0, 1.0);

    // get the angle between the light and surface normal
    float cosTheta = dot(L,N);

    // halfway vector was computed in vertex shader, and interpolated
    vec3 H = normalize(varyingHalfVectorG);

    float cosPhi = dot(H, N);

    vec3 ambient = ((globalAmbient * material.ambient) + (light.ambient * material.ambient)).xyz;
    vec3 diffuse = light.diffuse.xyz * material.diffuse.xyz * max(cosTheta, 0.0);
    vec3 specular = light.specular.xyz * material.specular.xyz * pow(max(cosPhi, 0.0), material.shininess*3.0);

    //Write the texture coordinates to the object\
    fragColor = vec4(ambient, 1.0) * 0.5 + texture(samp, tcG) * 0.5;

    float notInShadow = textureProj(shadowTex, shadow_coord);

    if (notInShadow == 1.0)
    {
        fragColor = vec4((ambient + diffuse + specular), 1.0) * 0.5 + texture(samp, tcG) * 0.5;
    }

    fragColor = mix(fog, fragColor, fogFactor);
}
